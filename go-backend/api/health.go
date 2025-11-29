/*
 * 健康检查API
 */

package api

import (
	"context"

	"github.com/cloudwego/hertz/pkg/app"
	"github.com/cloudwego/hertz/pkg/protocol/consts"
)

// HealthCheckHandler 健康检查处理
func HealthCheckHandler(c context.Context, ctx *app.RequestContext) {
	ctx.JSON(consts.StatusOK, map[string]interface{}{
		"success": true,
		"message": "服务正常运行",
	})
}